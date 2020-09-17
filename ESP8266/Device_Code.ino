#include <FS.h>
#include <ESP8266WiFi.h>
#include <Firebase.h>
#include <FirebaseArduino.h>
#include <FirebaseCloudMessaging.h>
#include <FirebaseError.h>
#include <FirebaseHttpClient.h>
#include <FirebaseObject.h>
#include <ESP8266HTTPClient.h>
#include <DNSServer.h>            //Library to start DNS server
#include <ESP8266WebServer.h>     //Library to start web server
#include <WiFiManager.h>          //https://github.com/tzapu/WiFiManager (Tutorial website)

float gas_sensor = A0;//Pin A0
int led_detect = 4;   //Pin D2
int led_warning = 2;  //Pin D4
int led_work = 14;    //pin D5
int fire_sensor = 16; //Pin D0
int buzzer = 13;      //Pin D7


#define DRD_TIMEOUT 10
#define DRD_ADDRESS 0
#define FIREBASE_HOST "FIREBASE HOST"
#define FIREBASE_AUTH "FIREBASE AUTH"

bool shouldSaveConfig = false;
bool gas;
bool flame;
bool list;
char topic_firebase[40];
int Flame;
int gas_value;
int gasstate = 0;
int registered;
int sizedata;

String serve = "FIREBASE SERVER KEY";
String ls, sd, pn;
String topic;

HTTPClient httpTOPIK;
void doitTOPIC(String paytitle, String pay, String top) {
  String data = "{";
  data = data + "\"to\": \"/topics/" + top + "\",";
  data = data + "\"data\": {";
  data = data + "\"alert\": \"" + pay + "\"";
//  data = data + "\"title\" : \"" + paytitle + "\" ";
  data = data + "} }";
      
  httpTOPIK.begin("http://fcm.googleapis.com/fcm/send");
  httpTOPIK.addHeader("Authorization", "key=" + serve);
  httpTOPIK.addHeader("Content-Type", "application/json");
  httpTOPIK.addHeader("Host", "fcm.googleapis.com");
  httpTOPIK.addHeader("Content-Length", String(pay.length()));
  httpTOPIK.POST(data);
  httpTOPIK.writeToStream(&Serial);
  httpTOPIK.end();
  Serial.println();
}

HTTPClient httpGasOnly;
void doitGasOnly(String title, String body, String top) {
  String data = "{";
  data = data + "\"to\": \"/topics/" + top + "\",";
  data = data + "\"notification\": {";
  data = data + "\"body\": \"" + body + "\"";
  data = data + "\"title\" : \"" + title + "\" ";
  data = data + "} }";
      
  httpGasOnly.begin("http://fcm.googleapis.com/fcm/send");
  httpGasOnly.addHeader("Authorization", "key=" + serve);
  httpGasOnly.addHeader("Content-Type", "application/json");
  httpGasOnly.addHeader("Host", "fcm.googleapis.com");
  httpGasOnly.addHeader("Content-Length", String(body.length()));
  httpGasOnly.POST(data);
  httpGasOnly.writeToStream(&Serial);
  httpGasOnly.end();
  Serial.println();
}

HTTPClient httpsms;
// USING SMS Gateway can be ignored
void dosms(String nohp)
{
  String postData;
  String alarm = "Alarm gas aktif";
  String pass = "pass";
  postData = "pass=" + pass + "&nohp=" + nohp + "&text=" + alarm;
  httpsms.begin("SMS GATEWAY LINK");
  httpsms.addHeader("Content-Type", "application/x-www-form-urlencoded");
  httpsms.POST(postData);
  httpsms.writeToStream(&Serial);
  httpsms.end();
  Serial.println();
}
void saveConfigCallback () {
  Serial.println("Should save config");
  shouldSaveConfig = true;
}
void setup() {
  Serial.begin(115200);
  pinMode(buzzer, OUTPUT);
  pinMode(led_detect, OUTPUT);
  pinMode(led_work, OUTPUT);
  pinMode(led_warning, OUTPUT);
  pinMode(fire_sensor, INPUT);

  delay(1000);
  
  if (SPIFFS.begin()) {
    Serial.println("mounted file system");
    if (SPIFFS.exists("/config.json")) {
      //file exists, reading and loading
      Serial.println("reading config file");
      File configFile = SPIFFS.open("/config.json", "r");
      if (configFile) {
        Serial.println("opened config file");
        size_t size = configFile.size();
        // Allocate a buffer to store contents of the file.
        std::unique_ptr<char[]> buf(new char[size]);

        configFile.readBytes(buf.get(), size);
        DynamicJsonBuffer jsonBuffer;
        JsonObject& json = jsonBuffer.parseObject(buf.get());
        json.printTo(Serial);
        if (json.success()) {
          Serial.println("\nparsed json");
          strcpy(topic_firebase, json["topic_firebase"]);
          Serial.println(topic_firebase);
        } else {
          Serial.println("failed to load json config");
        }
      }
    }
  } else {
    Serial.println("failed to mount FS");
  }
  
  WiFiManagerParameter custom_topic_firebase("Topic", "topic firebase", topic_firebase, 40);

  WiFiManager wifiManager;

  wifiManager.setSaveConfigCallback(saveConfigCallback);

  wifiManager.addParameter(&custom_topic_firebase);

  digitalWrite(led_warning, HIGH);
  if (!wifiManager.autoConnect("GasDetector", "gasdetector"))
  {
    Serial.println("failed to connect and hit timeout");
    delay(3000);
    //reset and try again, or maybe put it to deep sleep
    ESP.reset();
    delay(5000);
  }
  Serial.print("Connected to... :");
  Serial.println(WiFi.SSID());
  
  strcpy(topic_firebase, custom_topic_firebase.getValue());

  if (shouldSaveConfig) {
    Serial.println("saving config");
    DynamicJsonBuffer jsonBuffer;
    JsonObject& json = jsonBuffer.createObject();
    json["topic_firebase"] = topic_firebase;

    File configFile = SPIFFS.open("/config.json", "w");
    if (!configFile) {
      Serial.println("failed to open config file for writing");
    }

    json.printTo(Serial);
    json.printTo(configFile);
    configFile.close();
    //end save
  }
  topic = topic_firebase;
  if(topic == "")
  {
    Serial.println("Cannot find topic");
    WiFi.disconnect(true);
    delay(2000);
    ESP.restart();
  }
  delay(2000);
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  ls = "/topic/" + topic;
  sd = "/size/" + topic;
  pn = "/penerima/" + topic;
  registered = Firebase.getInt(ls + "/register");
  Serial.print("Data adalah ");
  Serial.println(registered);
  if (registered != 97)
  {
    setup_data_firebase();
  }
  delay(30000);
  digitalWrite(led_warning, LOW);
  digitalWrite(led_work, HIGH);
}

void setup_data_firebase()
{
  Serial.print("Setting up devices to firebase...");
  Firebase.set(ls, 0);
  Firebase.setInt(ls + "/register", 97);
  Firebase.setBool(ls + "/gas_sensor", true);
  Firebase.setBool(ls + "/flame_sensor", true);
  Firebase.setBool(ls + "/send_list", true);
  Firebase.set(pn, 0);
  Firebase.set(sd, 0);
}

void detect_gas()
{
  gas_value = analogRead(gas_sensor);
  Serial.println("gas sensor value");
  Serial.println(gas_value, DEC);
  if (gas_value > 700)
  {
    digitalWrite(led_warning, HIGH);
    digitalWrite(led_work, LOW);
    if (gasstate == 0)
    {
      Serial.println("Gas Detected!!! Help Me!!!");
      doitGasOnly("Gas Detector", "Gas has been detected", topic_firebase);
      list = Firebase.getBool(ls + "/send_list");
      if(list == true)
      {
        sendsms();
      }
      gasstate = 1;
    }
    else
    {
      
    }
  }
  else
  {
    digitalWrite(led_work, HIGH);
    digitalWrite(led_warning, LOW);
    gasstate = 0;
  }
}

void detect_flame()
{
  Flame = digitalRead(fire_sensor);
  if (Flame == HIGH) {
    Serial.println("No fire detected");
    digitalWrite(led_detect, LOW);
    digitalWrite(led_work, HIGH);
  }
  else
  {
    Serial.println("Fire detected");
    digitalWrite(led_detect, HIGH);
    digitalWrite(led_work, LOW);
    doitTOPIC("Title", "Fire has has been detected", topic_firebase);
    if(gas_value > 700)
    {
      tone(buzzer, 1000, 200);
    }
  }
}

void sendsms()
{
  sizedata = Firebase.getInt(sd);
  Serial.print("Jumlah data : ");
  Serial.println(sizedata);
  if(sizedata > 0)
  {
    for(int i = 1; i <= sizedata; i++)
    {
      Serial.println(Firebase.getString(pn + "/" + i + "/nohp" ));
      dosms(Firebase.getString(pn + "/" + i + "/nohp"));
      delay(3000);
    }
  }
}

void loop() {
  gas = Firebase.getBool(ls + "/gas_sensor");
  flame = Firebase.getBool(ls + "/flame_sensor");
  
  if(gas == true)
  {
    detect_gas();
  }
  if(flame == true)
  {
    detect_flame();
  }
  delay(1000);
}
