#!/bin/bash
# install.sh - Install claude-gh-standup to ~/.claude-gh-standup

set -e

INSTALL_DIR="$HOME/.claude-gh-standup"
COMMAND_LINK="$HOME/.claude/commands/claude-gh-standup.md"

# Helper: Check if we're in the actual claude-gh-standup repository
is_claude_gh_standup_repo() {
    [ -d ".git" ] && \
    git remote get-url origin 2>/dev/null | grep -qE "jetmobsol/claude-gh-standup(\.git)?$"
}

# Helper: Check if running interactively (not via curl|bash)
is_interactive() {
    [ -t 0 ]  # stdin is a terminal, not a pipe
}

# Helper: Prompt user with default for non-interactive mode
# Usage: prompt_yn "message" "default" -> sets REPLY
prompt_yn() {
    local message="$1"
    local default="${2:-n}"

    if is_interactive; then
        read -p "$message " -n 1 -r </dev/tty
        echo
    else
        echo "$message [non-interactive: $default]"
        REPLY="$default"
    fi
}

# Helper: Validate source directory has required files
validate_source() {
    local dir="$1"
    [ -f "$dir/scripts/Main.java" ] && \
    [ -d "$dir/prompts" ] && \
    [ -d "$dir/.claude/commands" ]
}

echo "Installing claude-gh-standup..."
echo ""

# 1. Check prerequisites
echo "Checking prerequisites..."
command -v jbang >/dev/null 2>&1 || {
    echo "❌ Error: jbang not found"
    echo "   Install: curl -Ls https://sh.jbang.dev | bash -s - app setup"
    exit 1
}
command -v gh >/dev/null 2>&1 || {
    echo "❌ Error: gh CLI not found"
    echo "   Install: https://cli.github.com"
    exit 1
}
command -v git >/dev/null 2>&1 || {
    echo "❌ Error: git not found"
    exit 1
}
echo "✓ Prerequisites satisfied"
echo ""

# 2. Determine installation source
# IMPORTANT: Only use current directory if we're BOTH interactive AND in the actual repo
# This prevents curl|bash from copying random directories
if is_interactive && is_claude_gh_standup_repo; then
    # Running interactively from the cloned claude-gh-standup repo
    SOURCE_DIR="$(pwd)"
    echo "Installing from current directory: $SOURCE_DIR"
else
    # Running via curl|bash OR not in the right repo - always clone fresh
    if ! is_interactive; then
        echo "Detected non-interactive mode (curl|bash) - cloning fresh copy..."
    else
        echo "Not in claude-gh-standup repo - cloning..."
    fi
    TEMP_DIR=$(mktemp -d)
    git clone --depth 1 https://github.com/jetmobsol/claude-gh-standup.git "$TEMP_DIR"
    SOURCE_DIR="$TEMP_DIR"
fi

# Validate source directory has expected structure
if ! validate_source "$SOURCE_DIR"; then
    echo "❌ Error: Invalid source directory - missing required files"
    echo "   Expected: scripts/Main.java, prompts/, .claude/commands/"
    echo "   This doesn't look like the claude-gh-standup repository"
    [ -n "$TEMP_DIR" ] && rm -rf "$TEMP_DIR"
    exit 1
fi
echo "✓ Source validated"

# 3. Install to fixed location
if [ -d "$INSTALL_DIR" ]; then
    echo "Existing installation found at $INSTALL_DIR"
    prompt_yn "Update existing installation? (y/n)" "y"
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Updating installation..."
        rsync -av --exclude='.git' --exclude='config.json' "$SOURCE_DIR/" "$INSTALL_DIR/"
        echo "✓ Updated successfully!"
    else
        echo "Installation cancelled"
        [ -n "$TEMP_DIR" ] && rm -rf "$TEMP_DIR"
        exit 0
    fi
else
    echo "Installing to $INSTALL_DIR..."
    mkdir -p "$INSTALL_DIR"
    rsync -av --exclude='.git' "$SOURCE_DIR/" "$INSTALL_DIR/"
    echo "✓ Installed to $INSTALL_DIR"
fi

# 4. Create symlink for Claude Code
echo ""
echo "Creating symlink for Claude Code slash command..."
mkdir -p "$HOME/.claude/commands"
if [ -L "$COMMAND_LINK" ] || [ -e "$COMMAND_LINK" ]; then
    rm -rf "$COMMAND_LINK"
fi
ln -s "$INSTALL_DIR/.claude/commands/claude-gh-standup.md" "$COMMAND_LINK"
echo "✓ Symlink created: $COMMAND_LINK → command file"

# 5. Make scripts executable
chmod +x "$INSTALL_DIR/scripts"/*.java 2>/dev/null || true

# 6. Offer to install shell aliases
echo ""
echo "═══════════════════════════════════════════════"
echo "Shell Aliases Installation"
echo "═══════════════════════════════════════════════"
echo ""
echo "This will add convenient aliases to your shell:"
echo "  • standup-yesterday - Quick yesterday report (Friday if Monday)"
echo "  • standup-week      - Last week's activity"
echo "  • standup           - General command with all flags"
echo ""
prompt_yn "Install shell aliases? (y/n)" "y"

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Detect shell from $SHELL environment variable
    SHELL_RC=""
    USER_SHELL=$(basename "$SHELL")

    case "$USER_SHELL" in
        zsh)
            SHELL_RC="$HOME/.zshrc"
            ;;
        bash)
            # Prefer .bashrc, fallback to .bash_profile
            if [ -f "$HOME/.bashrc" ]; then
                SHELL_RC="$HOME/.bashrc"
            elif [ -f "$HOME/.bash_profile" ]; then
                SHELL_RC="$HOME/.bash_profile"
            fi
            ;;
        fish)
            SHELL_RC="$HOME/.config/fish/config.fish"
            ;;
        ksh|mksh)
            SHELL_RC="$HOME/.kshrc"
            ;;
        *)
            # Unknown shell, leave SHELL_RC empty
            ;;
    esac

    # If config file doesn't exist but we know which shell, offer to create it
    if [ -n "$SHELL_RC" ] && [ ! -f "$SHELL_RC" ]; then
        echo "Shell config file $SHELL_RC doesn't exist yet."
        prompt_yn "Create it and add aliases? (y/n)" "y"
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            mkdir -p "$(dirname "$SHELL_RC")"
            touch "$SHELL_RC"
        else
            SHELL_RC=""  # Don't use it if user doesn't want to create
        fi
    fi

    if [ -z "$SHELL_RC" ]; then
        echo ""
        echo "Could not detect shell config file."
        echo "Detected shell: $USER_SHELL (from \$SHELL: $SHELL)"
        echo ""
        echo "Please manually add these aliases to your shell config:"
        echo ""
        cat <<'EOF'
# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
        echo ""
        echo "Common shell config files:"
        echo "  zsh:  ~/.zshrc"
        echo "  bash: ~/.bashrc or ~/.bash_profile"
        echo "  fish: ~/.config/fish/config.fish"
    else
        echo "Adding aliases to $SHELL_RC..."

        # Check if aliases already exist
        if grep -q "claude-gh-standup aliases" "$SHELL_RC" 2>/dev/null; then
            echo "⚠ Aliases already exist in $SHELL_RC (skipping)"
        else
            # Fish shell uses different alias syntax
            if [ "$USER_SHELL" = "fish" ]; then
                cat >> "$SHELL_RC" <<'EOF'

# === claude-gh-standup aliases ===
alias standup-yesterday 'jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week 'jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup 'jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
            else
                # Bash/Zsh/Ksh syntax
                cat >> "$SHELL_RC" <<'EOF'

# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
            fi
            echo "✓ Aliases added to $SHELL_RC"
            echo ""
            if [ "$USER_SHELL" = "fish" ]; then
                echo "Restart your shell or run: source $SHELL_RC"
            else
                echo "Run to activate aliases: source $SHELL_RC"
            fi
        fi
    fi
fi

# 7. Initialize config if desired
echo ""
echo "═══════════════════════════════════════════════"
echo "Configuration Setup"
echo "═══════════════════════════════════════════════"
echo ""
prompt_yn "Initialize configuration file now? (y/n)" "y"
if [[ $REPLY =~ ^[Yy]$ ]]; then
    jbang "$INSTALL_DIR/scripts/Main.java" --config-init
    echo "✓ Config initialized at ~/.claude-gh-standup/config.json"
fi

# Clean up temp directory if used
if [ -n "$TEMP_DIR" ] && [ -d "$TEMP_DIR" ]; then
    rm -rf "$TEMP_DIR"
fi

# 8. Success summary
echo ""
echo "═══════════════════════════════════════════════"
echo "✓ Installation Complete!"
echo "═══════════════════════════════════════════════"
echo ""
echo "Three ways to use claude-gh-standup:"
echo ""
echo "1. Claude Code slash command:"
echo "   /claude-gh-standup --yesterday"
echo ""
echo "2. Shell aliases (if installed):"
echo "   standup-yesterday"
echo "   standup-week"
echo "   standup --days 3"
echo ""
echo "3. Direct jbang call:"
echo "   jbang ~/.claude-gh-standup/scripts/Main.java --yesterday"
echo ""
echo "Next steps:"
if [ -n "$SHELL_RC" ]; then
    echo "  1. Activate aliases: source $SHELL_RC"
fi
echo "  2. Add directories: standup --config-add ~/projects/my-repo"
echo "  3. List directories: standup --config-list"
echo "  4. Generate report: standup-yesterday"
echo ""
echo "For help: standup --help"
echo ""
