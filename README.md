# How to use this library

- add custom maven repository in project's build.gradle
```
allprojects {
    repositories {
        ...
        maven { url 'https://dl.bintray.com/visualizeq/maven/' }
        ...
    }
}
```

- add dependencies in app's build.gradle
```
compile 'com.nasket.library:wdc1003:1.0.2'
```

- define WDC1003 variable
- call setup in `onCreate`
- implement WDC1003Interface and required method in activity (that want to use scanner)
```
public class MainActivity extends AppCompatActivity implements WDC1003Interface {
  private WDC1003 wdc1003 = new WDC1003();
  
  ...
  @Override
  public void message(String message) {
    // do something with message from scanner (it will be barcode data)
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ...
    wdc1003.setup(this);
    ...
    
    // turn scanner on
    wdc1003.turnOn();
    
    // turn scanner off
    wdc1003.turnOff();
  }
  ...
}
```
