{
  pkgs,
  flake,
  system,
  perSystem,
  ...
}:
perSystem.devshell.mkShell ({...}: let
  # use same gradle as the gradle wrapper
  gradle = pkgs.callPackage (pkgs.gradleGen (let
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
in {
  devshell = {
    name = ''♨️ Hotswap'';
    startup.pre-commit.text = ''if [ -z "''${CI:-}" ]; then ${flake.checks.${system}.linters.shellHook} fi'';
  };

  env = [
    {
      name = "JAVA_HOME";
      value = pkgs.jdk.home;
    }
  ];

  packages = [
    pkgs.jdk
    gradle
  ];

  commands = [
    {
      name = "build";
      help = "compiles, runs tests, and reports success or failure";
      command = ''gradle :clean :check :installDist'';
    }
    {
      name = "build-continuously";
      help = "automatically run build when files change";
      command = ''gradle --continuous :check :installDist'';
    }
    {
      name = "lint";
      help = "run all linters - or specific ones when passed as arguments";
      command = ''pre-commit run --all-files "''${@}"'';
    }
  ];
})
