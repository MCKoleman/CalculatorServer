SCR_DIR = src
OUT_DIR = bin

JFLAGS = -d $(OUT_DIR)/ -cp $(SCR_DIR)/
JC = javac

SRCS = $(SCR_DIR)/*.java
CLASSES = $(SRCS:$(SCR_DIR)/%.java=$(OUT_DIR)/%.class)
.SUFFIXES = .java

client: client.class
	jar cfm client.jar info/client_manifest.txt -C $(OUT_DIR) .

client.class: src/client.java
	$(JC) $(JFLAGS) src/client.java

server: server.class
	jar cfm server.jar info/server_manifest.txt -C $(OUT_DIR) .

server.class: src/server.java
	$(JC) $(JFLAGS) src/server.java

clean:
	$(RM) $(OUT_DIR)/*.class