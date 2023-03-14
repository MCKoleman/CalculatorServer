SCR_DIR = src
OUT_DIR = bin
INFO_DIR = info

JFLAGS = -d $(OUT_DIR)/ -cp $(SCR_DIR)/
JC = javac

.SUFFIXES = .java

client: client.class
	jar cfm client.jar $(INFO_DIR)/client_manifest.txt -C $(OUT_DIR) .

client.class: src/client.java
	$(JC) $(JFLAGS) src/client.java

server: server.class
	jar cfm server.jar $(INFO_DIR)/server_manifest.txt -C $(OUT_DIR) .

server.class: src/server.java
	$(JC) $(JFLAGS) src/server.java

clean:
	$(RM) $(OUT_DIR)/*.class