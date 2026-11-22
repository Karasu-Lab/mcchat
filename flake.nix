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

    devShells = builtins.mapAttrs (system: pkgs: {
      default = pkgs.mkShell {
        packages = [ pkgs.xvfb-run ];
        VK_ICD_FILENAMES = "${pkgs.mesa}/share/vulkan/icd.d/lvp_icd.x86_64.json";
        LIBGL_ALWAYS_SOFTWARE = "1";
        LIBGL_DRIVERS_PATH = "${pkgs.mesa}/lib/dri";
        __EGL_VENDOR_LIBRARY_FILENAMES = "${pkgs.mesa}/share/glvnd/egl_vendor.d/50_mesa.json";
        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
          pkgs.libGL
          pkgs.mesa
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
      };
    }) inputs.nixpkgs.legacyPackages;
  };
}
