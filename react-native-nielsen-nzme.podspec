require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-nielsen-nzme"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/simonso-nzme/react-native-nielsen-nzme.git", :tag => "#{s.version}" }

  s.library = "c++"

  s.source_files = "ios/**/*.{h,m,mm}"
  
  s.frameworks = "UIKit", "Foundation", "AdSupport", "JavascriptCore", "WebKit", "SystemConfiguration", "Security", "AVFoundation"

  s.dependency "React"
  s.dependency "NielsenAppSDK"
end
