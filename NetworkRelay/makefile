SCR_DIR = src
OUT_DIR = bin
INFO_DIR = info

JFLAGS = -d $(OUT_DIR)/ -cp $(SCR_DIR)/
JC = javac

.SUFFIXES = .java

sender: sender.class
	jar cfm sender.jar $(INFO_DIR)/sender_manifest.txt -C $(OUT_DIR) .

sender.class: src/sender.java
	$(JC) $(JFLAGS) src/sender.java

network: network.class
	jar cfm network.jar $(INFO_DIR)/network_manifest.txt -C $(OUT_DIR) .

network.class: src/network.java
	$(JC) $(JFLAGS) src/network.java

receiver: receiver.class
	jar cfm receiver.jar $(INFO_DIR)/receiver_manifest.txt -C $(OUT_DIR) .

receiver.class: src/receiver.java
	$(JC) $(JFLAGS) src/receiver.java

clean:
	$(RM) $(OUT_DIR)/*.class