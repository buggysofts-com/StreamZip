# StreamZip [![](https://jitpack.io/v/buggysofts-com/StreamZip.svg)](https://jitpack.io/#buggysofts-com/StreamZip)

A zip explorer library with similar functionality of standard java <b>ZipFile</b> class, but unlike <b>ZipFile</b>
class, it uses an input stream as its data source instead of a <b>File</b> object.

<br />

## Import

### Maven

Add JitPack repository to your <b>pom.xml</b> file

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Finally, add this dependency.

```
<dependency>
    <groupId>com.github.buggysofts-com</groupId>
    <artifactId>StreamZip</artifactId>
    <version>v1.0.0</version>
</dependency>
```

And you are done importing the library in your maven project.

<br />

### Gradle

Add JitPack repository to your project-level build.gradle file

```
...

allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Or, in newer gradle projects, specially in android, if you need to the add repository in settings.gradle file...

```
...

dependencyResolutionManagement {
    ...
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Finally, add this dependency to your app/module level build.gradle file

```
...

dependencies {
    ...
    implementation 'com.github.buggysofts-com:StreamZip:v1.0.0'
}
```

And you are done importing the library in your gradle project.

<br />

## Sample codes

To create an instance from a <b>FileInputStream</b> do something like...

```
StreamZip zip = new StreamZip(
    new FileInputStream(
        new File("/home/ragib/workspace/zips/a.zip")
    )
);
```

<br />

Then you can use different methods that are similar to the standard java ZipFile class. For example here are the
publicly available methods.

- ```size()``` Returns the total number of available entries.
- ```getComment()``` Returns the principal comment of the zip file.
- ```entries()``` Returns all the available entries as a list of <b>ZipEntry</b>.
- ```getInputStream(...)``` Opens(and returns) a bounded input stream currently positioning at the start of the
  requested entry's data block.
- ```close()``` Closes the zip file, and any subsequent call to <b>getInputStream(...)</b> will throw an exception.
  However, other methods of the class that are saved in memory will still be available after call to <b>close()</b>.

**Please Note**

- The **ZipEntry** we mentioned above is a part of this library and has similar methods as the standard **ZipEntry**
  class
  in java jdk.
- If you do not have a **ZipEntry** instance, and only have the name of the entry, you can use the minimal
  constructor (
  i.e.  ```ZipEntry(String name)```) to obtain an input stream. Of course, you would get an exception if the entry does
  not
  exist.

<br />

### Usage

If you are wondering why the hell we would need a zip explorer that takes **FileInputStream** as its source, after all,
if you can get a file input stream, you can get a **File** object as well, so why use this library?

Well... there can be a lots of usage. Perhaps the biggest, and in my opinion, the best usage is on Android. As you may
know, Android is very restrictive these days, and will not give you read access through classic **File** objects if you
want to read a file residing outside an app's private storage area, specially on external storage devices. Instead, the
current way to access a file (in Android) these days is to use something called **DocumentFile**. We can get an input
stream with the code ```InputStream in = getContentResolver().openInputStream(documentFile.getUri())```. Then we use our
StreamZip constructor to obtain the zip explorer with something like

```
StreamZip zip = new StreamZip((FileInputStream)in);
```

<br />

### Performance

The performance is similar to the Standard **ZipFile** class. Before this, the only way to read a zip file in this kind
of situation was to use the **ZipInputStream** class which basically reads every byte in its way to get to the next
entry. That is, to list, or to get data of all the entries of a zip file, it is equivalent of reading the whole file.
Imagine you have to read some metadata within some big zip files, may be 100 zip files, think how much time it would
take!
Of course, you can use some caching technique, which I was doing for a long time, in fact there is still a library in
the git repo, which does exactly that. But in any way, that is not enough, it takes a lot of memory, and the performance
is limited to many constraints.

<br />

Please share & rate the library if you find it useful.

### Happy coding!