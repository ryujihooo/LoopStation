import paho.mqtt.client as mqtt
import time
from pydub import AudioSegment

song = AudioSegment.from_file('/home/pi/Desktop/source.3gp')
song = song-100
num=0
def on_connect(client,userdata,flags,rc):
    client.subscribe("SOURCE")
    
def on_message(client,userdata,msg):
    global song
    global num
    num+=1
    print("message arrive")
    fw=open('temp.3gp','wb')
    #fw=open('source.3gp','wb')
    fw.write(msg.payload)
    fw.close()
    
    data = AudioSegment.from_file('/home/pi/Desktop/temp.3gp',formet='arm')
    song  = song.overlay(data*2)
    song.export('/home/pi/Desktop/source.3gp')
    fr=open('source.3gp','rb')
    sendData = fr.read()
    fr.close()
    mqttClient.publish("CTRL-SPEAKER",sendData)
    #mqttClient.publish("CTRL-SPEAKER",msg.payload)

mqttClient = mqtt.Client("LoopStation")
mqttClient.on_connect=on_connect
mqttClient.on_message = on_message
mqttClient.connect("localhost",1883)

mqttClient.loop_start()

while True:
    print("MIKE ON")
    mqttClient.publish("CTRL-MIKE","MIKE ON")
    time.sleep(20)
    print("MIKE OFF")
    mqttClient.publish("CTRL-MIKE","MIKE OFF")
    time.sleep(62)