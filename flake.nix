{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

    systems.url = "github:nix-systems/default";

    blueprint.url = "github:numtide/blueprint";
    blueprint.inputs.nixpkgs.follows = "nixpkgs";
    blueprint.inputs.systems.follows = "systems";

    devshell.url = "github:numtide/devshell";
    devshell.inputs.nixpkgs.follows = "nixpkgs";

    git-hooks-nix.url = "github:cachix/git-hooks.nix";
  };
  outputs = inputs:
    inputs.blueprint {
      inherit inputs;
      prefix = "nix";
      nixpkgs.overlays = [
        (final: prev: {
          jdk = prev.jdk17_headless;
          jre_headless = final.jdk;
        })
      ];
    };
}
