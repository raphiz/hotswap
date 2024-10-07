{pkgs, ...}: {
  imports = [
    ./kotlin.nix
  ];

  pre-commit.hooks = {
    alejandra.enable = true;
  };

  scripts.lint.exec = ''pre-commit run --all-files "''${@}"'';
}
