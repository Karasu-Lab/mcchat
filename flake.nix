{
  description = "A very basic flake";

  inputs = {
    nixpkgs.url = "https://channels.nixos.org/nixpkgs-unstable/nixexprs.tar.zst";
  };

  outputs = inputs: {
    packages = builtins.mapAttrs (system: pkgs: {
      hello = pkgs.hello;

      default = inputs.self.packages.${system}.hello;
    }) inputs.nixpkgs.legacyPackages;

    devShells = builtins.mapAttrs (
      system: pkgs:
      let
        clientLibraries = [
          pkgs.libGL
          pkgs.vulkan-loader
          pkgs.libx11
          pkgs.libxext
          pkgs.libxcursor
          pkgs.libxrandr
          pkgs.libxi
          pkgs.libxxf86vm
          pkgs.libxrender
          pkgs.libxkbcommon
          pkgs.openal
          pkgs.alsa-lib
        ];
      in
      {
        default = pkgs.mkShell {
          packages = [
            pkgs.gradle
            pkgs.jdk25
          ];
          JAVA_HOME = "${pkgs.jdk25}";
          LD_LIBRARY_PATH = "${pkgs.lib.makeLibraryPath clientLibraries}:/run/opengl-driver/lib";
        };

        software = pkgs.mkShell {
          packages = [ pkgs.xvfb-run ];
          VK_ICD_FILENAMES = "${pkgs.mesa}/share/vulkan/icd.d/lvp_icd.x86_64.json";
          LIBGL_ALWAYS_SOFTWARE = "1";
          LIBGL_DRIVERS_PATH = "${pkgs.mesa}/lib/dri";
          __EGL_VENDOR_LIBRARY_FILENAMES = "${pkgs.mesa}/share/glvnd/egl_vendor.d/50_mesa.json";
          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath (clientLibraries ++ [ pkgs.mesa ]);
        };
      }
    ) inputs.nixpkgs.legacyPackages;
  };
}
