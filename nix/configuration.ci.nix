{pkgs, ...}: {
  imports = [
    ./java.nix
  ];

  packages = [
    pkgs.bashInteractive # see https://github.com/NixOS/nix/issues/6982#issuecomment-1236743200
  ];

  pre-commit.hooks = {
    alejandra.enable = true;

    convco.enable = true;
  };

  scripts.lint.exec = ''pre-commit run --all-files "''${@}"'';
}
