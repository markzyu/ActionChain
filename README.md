# ActionChain
[![](https://jitpack.io/v/C4Phone/SmartActionChain.svg)](https://jitpack.io/#C4Phone/SmartActionChain) 
[![Circle CI](https://circleci.com/gh/C4Phone/ActionChain.svg?style=shield)](https://circleci.com/gh/C4Phone/ActionChain)


A substitute for ```AsyncTask```. Also a "variant" of ```JDeferred``` where you could easily specify the thread to run actions on.
For type-safe ActionChains, please refer to [TActionChain](https://github.com/C4Phone/ActionChain/blob/master/src/zyu19/libs/action/chain/TActionChain.java) class, and [TActionChainTest](https://github.com/C4Phone/ActionChain/blob/master/tests/zyu19/libs/action/chain/tests/TActionChainTest.java)


## Sample Code :)

#### Example 0:
The original AsyncTask version of code was created by [Graham Smith](http://stackoverflow.com/users/649979/graham-smith) on [Stack Overflow](http://stackoverflow.com/questions/9671546/asynctask-android-example).

```Java
public class AsyncTaskActivity extends Activity implements OnClickListener {

    Button btn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btn = (Button) findViewById(R.id.button1);
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new LongOperation().execute("");
            }
        });
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            TextView txt = (TextView) findViewById(R.id.output);
            txt.setText("Executed"); // txt.setText(result);
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}
```

The basic idea is to avoid instantiating abstract classes, which are too lengthy code. And the tool to partially accomplish this goal is the new feature brought in Java 8 -- lambda. (In Java 7, we could get lambda support using RetroLambda.)

Basically as long as the abstract class has ***only one abstract method*** it could be shortened like this:

The original code:
```Java
        btn = (Button) findViewById(R.id.button1);
        btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new LongOperation().execute("");
            }
        });
```
The simplified code using Java 8 or Retrolambda:
```Java
        btn = (Button) findViewById(R.id.button1);
        btn.setOnClickListener(view -> new LongOperation().execute(""));
```

This convertion is available for ```.setOnClickListener``` because the abstract class only needs one function. ***However AsyncTask could not be directly shortened as a simple Lambda*** because it needs 4 functions, all of which are important and cannot be ignored, too.

Therefore we need a new interface similar to ```AsyncTask``` that helps building something similar while enabling developers to use lambdas. And ***the new interface, ```ActionChain``` looks like this:***
```Java
public class AsyncTaskActivity extends Activity implements OnClickListener {

    Button btn;

    // create a ActionChain factory using Android API (runOnUiThread) and 2 extra worker threads.
    ActionChainFactory chainFactory = new ActionChainFactory(uiCode -> runOnUiThread(uiCode), Executors.newFixedThreadPool(2));
    TextView txt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        txt = (TextView) findViewById(R.id.output);
        btn = (Button) findViewById(R.id.button1);
        btn.setOnClickListener(view -> onBtnClick());
    }

    void onBtnClick() {
        // The following code is using ActionChain
        // The ").xxx" style may look strange but that's the only way to obey most IDEs' indentation rule.

        chainFactory.get(
        ).uiConsume(obj -> {                                // equivalent to onPreExecute, could be deleted
        }).netThen(obj -> {                                 // equivalent to doInBackground
            for(int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
            return "Executed";
        }).uiThen((String result) -> txt.setText(result)    // equivalent to onPostExecute
        ).start();
    }
}
```

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


