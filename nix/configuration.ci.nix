{pkgs, ...}: {
  imports = [
    ./kotlin.nix
  ];

  scripts.lint.exec = ''pre-commit run --all-files "''${@}"'';
}
