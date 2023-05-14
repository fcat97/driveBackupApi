# Drive Backup Api

---

A simple library to upload, download and delete file in user's google drive app specific folder.



### How to use?

__Step 1:__ Setup Google Cloud Project

- Log in to [Google Cloud Console](https://console.cloud.google.com/)

- Create/Select a project

- Navigate to `APIs & Services` > `Enable APIs & Services`

- Search for `Google Drive API` and Enable it.

- Now navigate to `APIs & Services` > `OAuth Concent Screen` and configure it. For now, provide only the `App name`, `User support Email` and a email for `Developer contact information`. Save and continue.

- Configure the scope, click on `Add or Remove scope` and add `auth/drive.appdata` scope. Save and continue.

- Add a test user. Then save and continue.

- Review the information, if everything is okay, back to Dashboard.



__Step 2:__ Create credentials

- Navigate to `APIs & Services` > `Credentials` page.

- `Create Credentials` > `Create OAuth client ID`. 

- Select `Web Application` as Application type (*mandatory*). You'll need the credential ID to use this project. Just give a name, no other info needed for now. Click on `Create`.

- Similarly create a credential for your app. Just select `Android App` instead of `Web Application` this time.

Now that you've configured the cloud project, let's configure the app.



__Step 3:__ Configure android app

1. add this library in your app level gradle file: `implementation('com.github.fcat97:driveBackupApi:1.0.8')`

2. If your `minSdk` version is below `25` you may need to add Google's guava library manually as well.    

3. Also you need to exclude `/META-INF/AL2.0` and `/META-INF/DEPENDENCIES` to work. 



```groovy
android {
      packagingOptions {
          excludes += '/META-INF/{AL2.0,LGPL2.1}'
          excludes += '/META-INF/DEPENDENCIES'
      }
  }
  
  dependencies {
      implementation('com.github.fcat97:driveBackupApi:1.0.8')
      
      // if minSdk < 25
      // https://stackoverflow.com/a/71085378/8229399
      implementation("com.google.guava:guava") {
          version {
              strictly '31.1-android'
          }
      }
  }
```

Your project is configured.



**Step 4:** How to use the library?

First create an instance of `GoogleDriveBackupManager` in any activity. <u><strong>You must initialize it before <code>onCreate()</code> of the activity</strong>.</u> This is because the library uses `registerActivityForResult()` internally which requires to attach it with the activity before `onCreate()` is called.



**NOTE**: The `credentialID` is the `Web Application's` credential id, not the android's one.



All done. Now you can use the `GoogleDriveBackupManager` and all of its methods. All the methods are lifecycle aware. All the network operations are also run on worker thread. Just remember to add network permission in the manifest file.





Happy coding...
