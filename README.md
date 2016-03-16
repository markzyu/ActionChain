# ActionChain
[![](https://jitpack.io/v/C4Phone/SmartActionChain.svg)](https://jitpack.io/#C4Phone/SmartActionChain) 
[![Circle CI](https://circleci.com/gh/C4Phone/ActionChain.svg?style=shield)](https://circleci.com/gh/C4Phone/ActionChain)


A substitute for ```AsyncTask```.
For type-safe ActionChains, please refer to [TActionChain](https://github.com/C4Phone/ActionChain/blob/master/src/zyu19/libs/action/chain/TActionChain.java) class, and [TActionChainTest](https://github.com/C4Phone/ActionChain/blob/master/tests/zyu19/libs/action/chain/tests/TActionChainTest.java)


## Sample Code :)

#### Example 1:

```Realm``` database caused some trouble for us because it's required to be run on the same thread and defaults to run on *UI thread*, which might affect user experience. But ```ActionChain``` enables you to restrict ```Realm``` to run only on worker threads, in a simple yet safe way:

##### Encapsulation of ```Realm```
```Java
/**
 * Created on 3/14/16 
 *
 * Note: PureAction<I,O> is interface for a function that takes an argument of type I and returns an object of type O
 *       ReadOnlyChain is the object representing a launched ActionChain.
 *       (ActionChain is not launched until calling ".start()")
 */
public class RealmAccess implements PureAction<PureAction<Realm, ?>, ReadOnlyChain> {

    @Inject
    ActionChainFactory chainFactory;

    @Inject
    Context context;

    @Override
    public ReadOnlyChain process(PureAction<Realm, ?> editor) throws Exception {
        return chainFactory.get(fail -> fail.getCause().printStackTrace()
        ).netThen(() -> {
            Realm realm = Realm.getInstance(new RealmConfiguration.Builder(context)
                    .name(context.getString(R.string.realm_filename))
                    .deleteRealmIfMigrationNeeded()
                    .build());
            Object result = editor.process(realm);
            realm.close();
            return result;
        }).start();
    }
}
```

##### Usage of ```ReamAccess```

Example 1.1:

```Java
// Retrieve current user
chainFactory.get(fail -> fail.getCause().printStackTrace()
).netThen(() -> realmAccess.process(realm -> {
    return realm.where(Person.class)
            .equalTo("personId", userStore.getMostRecentUserId())
            .findFirst().getName();
})).uiConsume((String name) -> {
    mUserName.setText(name);
}).start();
```

Example 1.2:

```Java
chain.netThen((String userName) -> {
    Response<ResponseBody> response = service.getCurrentPerson().execute();
    Person person = new Person();
    if (response.code() != 200)
        throw new IOException(response.message());
    ResponseBody responseBody = response.body();
    JSONObject jsonObject = new JSONObject(responseBody.string());

    String ourUserID = jsonObject.getString("_id");

    // Set user ID in preferences
    userStore.setUserId(ourUserID);

    Photo photo = new Photo();
    photo.setPhotoUrl(jsonObject.getString("avatarUrl"));
    photo.setType(Photo.TYPE_AVATAR);
    person.setName(userName);
    person.setAvatar(photo);
    person.setFacebookId(jsonObject.getString("facebookId"));
    person.setCreatedAt(DateTimeConverter.toDate(jsonObject.getString("createdAt")));
    person.setPersonId(ourUserID);

    Log.d("UpUserInfo", "A" + person);

    return person;
}).netThen((Person newPerson) -> realmAccess.process(realm -> {
    try {
        Log.d("UpUserInfo", newPerson == null ? "null" : newPerson.toString());
        // Set user details in database
        realm.beginTransaction();
        Person result = realm.where(Person.class)
                .equalTo("personId", newPerson.getPersonId())
                .findFirst();
        if (result != null)
            result.removeFromRealm();
        realm.copyToRealm(newPerson);
        realm.commitTransaction();
        // MAYBE NOT NEEDED: bus.post(new UserInfoUpdatedEvent(result));
        return newPerson;
    } catch (Exception err) {
        // Maybe errorHolder.retry() ?

        if (realm.isInTransaction())
            realm.cancelTransaction();
        err.printStackTrace();
        throw err;
    }
}));
```


#### Example 2
[Android application](https://github.com/TakefiveInteractive/Ledger-Android/tree/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa), used with retrolambda

[AsyncTask version](https://github.com/TakefiveInteractive/Ledger-Android/blob/3402d6c3f4272881d4d6df04648237646b8ab588/app/src/main/java/com/takefive/ledger/WelcomeActivity.java#L124)

Untangled version:

1. [Creating and using action chain](https://github.com/TakefiveInteractive/Ledger-Android/blob/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa/app/src/main/java/com/takefive/ledger/WelcomeActivity.java#L128)
2. [Creating a set of actions for .use()](https://github.com/TakefiveInteractive/Ledger-Android/blob/5b00fe9ac42685581a83fbb49fe1f1ef89cc35fa/app/src/main/java/com/takefive/ledger/task/UpdateUserInfoTask.java#L27)

 - Althought the event bus version also eliminates callback hell, ```AsyncTask``` forces developers to think in a un-natural way, where:
   - UI code and other operations cannot be mixed together. (Otherwise the code will contain lots of small pieces of ```AsyncTask```s)
   - It's not straightforward to understand the sequence of the Tasks
 - ActionChain Bonus:
   - Once-for-all exception handling for all the threads, as long as they are in the same task.



## Diagram illustration
#### We turn this tangled thought:

<img src="https://cloud.githubusercontent.com/assets/7296488/12837276/8ef590a2-cb86-11e5-8b02-c4c40f6805bd.png" width="512px" height="284px" />
#### into this linear, straightforward one:

<img src="https://cloud.githubusercontent.com/assets/7296488/12837277/92e391fa-cb86-11e5-8fcc-62fd0620c0ed.png" width="512px" height="323px" />

## Documentations

Please visit [this github.io website](http://c4phone.github.io/ActionChain/).

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


