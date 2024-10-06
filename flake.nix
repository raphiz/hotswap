{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/master";
  };

  outputs = {
    self,
    nixpkgs,
    ...
  }: let
    systems = ["x86_64-linux" "x86_64-darwin" "aarch64-darwin" "aarch64-linux"];
    forAllSystems = function: nixpkgs.lib.genAttrs systems (system: function nixpkgs.legacyPackages.${system});
  in {
    devShells = forAllSystems (pkgs: let
      jdk = pkgs.jdk21_headless;

      # use same gradle as the gradle wrapper
      gradle = pkgs.callPackage (pkgs.gradleGen (let
        wrapperProperties = builtins.readFile ./gradle/wrapper/gradle-wrapper.properties;
        lines = pkgs.lib.strings.splitString "\n" wrapperProperties;
      in {
        defaultJava = jdk;

        # version from gradle-wrapper.properties
        version = let
          distributionUrlLine = builtins.head (builtins.filter (line: line != null && builtins.match "distributionUrl=.*" line != null) lines);
          versionMatch = builtins.match ".*/gradle-([^-]*)-bin.zip" distributionUrlLine;
          versionValue = builtins.elemAt versionMatch 0;
        in
          versionValue;

        # hash from gradle-wrapper.properties
        hash = let
          sha256SumLine = builtins.head (builtins.filter (line: line != null && builtins.match "distributionSha256Sum=.*" line != null) lines);
          sha256Hex = builtins.elemAt (builtins.match "distributionSha256Sum=(.*)" sha256SumLine) 0;
          formattedHash = "sha256:" + sha256Hex;
        in
          formattedHash;
      })) {};
    in {
      default = pkgs.mkShellNoCC {
        buildInputs = [jdk gradle];
      };
    });
  };
}
