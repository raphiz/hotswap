{
  pkgs,
  config,
  ...
}: {
  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk;

    gradle.enable = true;
    # use same gradle as the gradle wrapper
    gradle.package = pkgs.callPackage (pkgs.gradleGen (let
      wrapperProperties = builtins.readFile ../gradle/wrapper/gradle-wrapper.properties;
      lines = pkgs.lib.strings.splitString "\n" wrapperProperties;
    in {
      defaultJava = pkgs.jdk;

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
  };

  # Used primarily for gradle build scripts
  pre-commit.hooks.ktlint = {
    enable = true;
    name = "ktlint";
    entry = "${pkgs.ktlint}/bin/ktlint --format";
    files = "\\.(kt|kts)$";
    language = "system";
  };
}
