# ActionChain
[![](https://jitpack.io/v/C4Phone/SmartActionChain.svg)](https://jitpack.io/#C4Phone/SmartActionChain) 
[![Circle CI](https://circleci.com/gh/C4Phone/ActionChain.svg?style=shield)](https://circleci.com/gh/C4Phone/ActionChain)


Event handling with untangled thoughts.


## Diagram illustration
#### We turn this tangled thought:

<img src="https://cloud.githubusercontent.com/assets/7296488/12837276/8ef590a2-cb86-11e5-8b02-c4c40f6805bd.png" width="512px" height="284px" />
#### into this linear, straightforward one:

<img src="https://cloud.githubusercontent.com/assets/7296488/12837277/92e391fa-cb86-11e5-8fcc-62fd0620c0ed.png" width="512px" height="323px" />

## Sample Code :)

[Android application](https://github.com/TakefiveInteractive/Ledger-Android/tree/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa), used with retrolambda

1. [Creating and using action chain](https://github.com/TakefiveInteractive/Ledger-Android/blob/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa/app/src/main/java/com/takefive/ledger/WelcomeActivity.java#L128)
2. [Creating a set of actions for .use()](https://github.com/TakefiveInteractive/Ledger-Android/blob/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa/app/src/main/java/com/takefive/ledger/task/UpdateUserInfoTask.java#L27)

You could also cntract the code above with the [tangled version](https://github.com/TakefiveInteractive/Ledger-Android/blob/3402d6c3f4272881d4d6df04648237646b8ab588/app/src/main/java/com/takefive/ledger/WelcomeActivity.java#L124) using event bus.

 - Althought the event bus version also eliminates callback hell, the thought behind the code is scattered, tangled and a little confusing:
   - It's not straightforward to understand the sequence of the Tasks
 - ActionChain Bonus:
   - We always get to choose the thread to run a task, without having to send more events, or create more named functions/callbacks.

## Documentations

Please visit [this github.io website](http://c4phone.github.io/SmartActionChain/).

It's recommended to begin by reading the documentations about ```ChainStyle``` and ```ErrorHolder```.

## Get Started

Thanks to JitPack, we could all import this library using standard syntax!

#### For gradle:
##### 1. Add the JitPack repository (please edit /build.gradle)
```groovy
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```
##### 2. Add the dependency (please edit /&lt;module name&gt;/build.gradle)
```groovy
dependencies {
  compile 'com.github.C4Phone:SmartActionChain:v0.3'
}
```
#### For Maven:
##### 1. Add the JitPack repository
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
##### 2. Add the dependency
```xml
	<dependency>
	    <groupId>com.github.C4Phone</groupId>
	    <artifactId>SmartActionChain</artifactId>
	    <version>v0.3</version>
	</dependency>
```


