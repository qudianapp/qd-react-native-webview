
# react-native-webviewplus

## Getting started

`$ npm install react-native-webviewplus --save`

### Mostly automatic installation

`$ react-native link react-native-webviewplus`

### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.phoobobo.rnwebviewplus.ReactWebViewPlusPackage;` to the imports at the top of the file
  - Add `new ReactWebViewPlusPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-webviewplus'
  	project(':react-native-webviewplus').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-webviewplus/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      implementation project(':react-native-webviewplus')
  	```


## Usage
```javascript
import RNWebviewplus from 'react-native-webviewplus';

// TODO: What to do with the module?
RNWebviewplus;
```
  