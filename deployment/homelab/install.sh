#!/usr/bin/env bash
# Homelab installer for the private AI chat service (Day 30).
# Usage: ./install.sh {detect|ollama|help}
set -euo pipefail

ACTION="${1:-help}"

detect() {
  echo "== OS =="
  uname -a || true
  if [ -f /etc/os-release ]; then . /etc/os-release; echo "$PRETTY_NAME"; fi
  echo "== CPU / RAM =="
  nproc 2>/dev/null || true
  free -h 2>/dev/null || true
  echo "== Listening ports (11434/8081) =="
  (ss -ltn 2>/dev/null || netstat -ltn 2>/dev/null || true) | grep -E '11434|8081' || echo "none"
  echo "== Ollama =="
  command -v ollama >/dev/null && ollama --version || echo "not installed"
  echo "== Java =="
  java -version 2>&1 | head -1 || echo "java not installed"
}

install_ollama() {
  if command -v ollama >/dev/null; then
    echo "Ollama already installed: $(ollama --version)"
    return
  fi
  echo "Installing Ollama (official script)..."
  curl -fsSL https://ollama.com/install.sh | sh
  echo "Start Ollama and pull the model: ollama pull qwen3:1.7b"
}

case "$ACTION" in
  detect) detect ;;
  ollama) install_ollama ;;
  *) echo "Usage: $0 {detect|ollama}" ;;
esac
