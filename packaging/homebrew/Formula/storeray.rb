class Storeray < Formula
  desc "A lightweight CLI for App Store Connect & Google Play metadata management"
  homepage "https://github.com/idleray/storeray"
  version "__VERSION__"
  url "https://github.com/idleray/storeray/releases/download/v#{version}/storeray-v#{version}.zip"
  sha256 "__SHA256__"

  depends_on "openjdk@17"

  def install
    libexec.install Dir["*"]
    bin.install_symlink libexec/"bin/storeray"
  end

  test do
    assert_match "storeray", shell_output("#{bin}/storeray --help")
  end
end
