#!/bin/bash
# install.sh - Install claude-gh-standup to ~/.claude-gh-standup

set -e

INSTALL_DIR="$HOME/.claude-gh-standup"
COMMAND_LINK="$HOME/.claude/commands/claude-gh-standup"

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
if [ -d ".git" ]; then
    # Running from cloned repo
    SOURCE_DIR="$(pwd)"
    echo "Installing from current directory: $SOURCE_DIR"
else
    # Need to clone
    echo "Cloning repository..."
    TEMP_DIR=$(mktemp -d)
    git clone https://github.com/jetmobsol/claude-gh-standup.git "$TEMP_DIR"
    SOURCE_DIR="$TEMP_DIR"
fi

# 3. Install to fixed location
if [ -d "$INSTALL_DIR" ]; then
    echo "Existing installation found at $INSTALL_DIR"
    read -p "Update existing installation? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Updating installation..."
        rsync -av --exclude='.git' --exclude='config.json' "$SOURCE_DIR/" "$INSTALL_DIR/"
        echo "✓ Updated successfully!"
    else
        echo "Installation cancelled"
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
ln -s "$INSTALL_DIR" "$COMMAND_LINK"
echo "✓ Symlink created: $COMMAND_LINK → $INSTALL_DIR"

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
read -p "Install shell aliases? (y/n) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Detect shell
    SHELL_RC=""
    if [ -n "$ZSH_VERSION" ]; then
        SHELL_RC="$HOME/.zshrc"
    elif [ -n "$BASH_VERSION" ]; then
        if [ -f "$HOME/.bashrc" ]; then
            SHELL_RC="$HOME/.bashrc"
        elif [ -f "$HOME/.bash_profile" ]; then
            SHELL_RC="$HOME/.bash_profile"
        fi
    fi

    if [ -z "$SHELL_RC" ]; then
        echo ""
        echo "Could not detect shell config file."
        echo "Please manually add these aliases to your shell config:"
        echo ""
        cat <<'EOF'
# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
    else
        echo "Adding aliases to $SHELL_RC..."

        # Check if aliases already exist
        if grep -q "claude-gh-standup aliases" "$SHELL_RC" 2>/dev/null; then
            echo "⚠ Aliases already exist in $SHELL_RC (skipping)"
        else
            cat >> "$SHELL_RC" <<'EOF'

# === claude-gh-standup aliases ===
alias standup-yesterday='jbang ~/.claude-gh-standup/scripts/Main.java --yesterday'
alias standup-week='jbang ~/.claude-gh-standup/scripts/Main.java --last-week'
alias standup='jbang ~/.claude-gh-standup/scripts/Main.java'
EOF
            echo "✓ Aliases added to $SHELL_RC"
            echo ""
            echo "Run to activate aliases: source $SHELL_RC"
        fi
    fi
fi

# 7. Initialize config if desired
echo ""
echo "═══════════════════════════════════════════════"
echo "Configuration Setup"
echo "═══════════════════════════════════════════════"
echo ""
read -p "Initialize configuration file now? (y/n) " -n 1 -r
echo
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
