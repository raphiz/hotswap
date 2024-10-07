{
  /*
  This flake contains all boilerplate and rarely needs to be looked at
  Actual configuration is done in the following, externalized files:

  ./nix/configuration.ci.nix: minimal dependencies required on CI system
  ./nix/configuration.dev.nix: dependencies for local development
  ./nix/overlay.nix: customization of nixpkgs
  */

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/master";
    devenv.url = "github:cachix/devenv";

    #required for building linux container images
    #otherwise `nix flake show --impure` fails to evaluate
    nix2container.url = "github:nlewo/nix2container";
    nix-mk-shell-bin.url = "github:rrbutani/nix-mk-shell-bin";
  };

  outputs = inputs @ {
    self,
    flake-parts,
    ...
  }:
    flake-parts.lib.mkFlake {inherit inputs;} {
      systems = ["x86_64-linux" "x86_64-darwin" "aarch64-darwin" "aarch64-linux"];
      imports = [inputs.devenv.flakeModule];
      perSystem = {
        config,
        self',
        inputs',
        pkgs,
        system,
        ...
        # Per-system attributes can be defined here. The self' and inputs'
        # module parameters provide easy access to attributes of the same
        # system. Documentation: https://flake.parts/module-arguments.html
        # inputs': https://flake.parts/module-arguments.html#inputs
        # self': https://flake.parts/module-arguments.html#self
      }: {
        # define development shells
        devenv.shells.ci = import ./nix/configuration.ci.nix;
        devenv.shells.dev = import ./nix/configuration.dev.nix;
        devShells.default = self'.devShells.dev;

        # The following snippet is needed when using overlays.
        # The overlay can be inlined or referenced from flake.overlays.*
        # Explanation: https://flake.parts/overlays.html#consuming-an-overlay
        _module.args.pkgs = import self.inputs.nixpkgs {
          inherit system;
          overlays = [self.overlays.default];
          config.allowUnfree = true;
        };
      };
      flake = {
        overlays.default = import ./nix/overlay.nix;
      };
    };
}
