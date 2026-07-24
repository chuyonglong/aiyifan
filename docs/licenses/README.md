# GPL Source And License Notice

The complete Aiyifan application is distributed under GPL-3.0-or-later. The source for every release, including the pinned submodule and the script used to produce the bundled native library, must be made available with the APK from the same release location.

This application bundles `app/libs/libbox.aar`, built from the `third_party/sing-box` submodule at commit `4626aa2cb07db5a453f689cf348ba9d327e07820` (`v1.12.11`).

sing-box is licensed under GPL-3.0-or-later. This repository distributes the complete corresponding source through that pinned submodule and includes the upstream license text in `sing-box-GPL-3.0-or-later.txt`.

To reproduce the Android library, initialize the submodule and run `powershell -ExecutionPolicy Bypass -File .\tools\build-libbox.ps1` with Android SDK, NDK, Go, and OpenJDK 17 installed. The checked-in library SHA-256 is `355D385D8159E8877C2019166CB1B3BCBAEF4F390531DFE4C2D8A7FD5A6C855B`.
