{
  inputs,
  system,
  pkgs,
  ...
}:
inputs.git-hooks-nix.lib.${system}.run {
  src = ../../.;
  hooks = {
    alejandra.enable = true;
    convco.enable = true;

    ktlint = {
      enable = true;
      name = "ktlint";
      entry = "${pkgs.ktlint}/bin/ktlint --format";
      files = "\\.(kt|kts)$";
      language = "system";
    };
  };
}
