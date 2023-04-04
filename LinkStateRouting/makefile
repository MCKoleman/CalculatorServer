SCR_DIR = src
OUT_DIR = bin
INFO_DIR = info

JFLAGS = -d $(OUT_DIR)/ -cp $(SCR_DIR)/
JC = javac

.SUFFIXES = .java

linkstate: linkstate.class
	jar cfm linkstate.jar $(INFO_DIR)/linkstate_manifest.txt -C $(OUT_DIR) .

linkstate.class: src/linkstate.java
	$(JC) $(JFLAGS) src/linkstate.java

clean:
	$(RM) $(OUT_DIR)/*.class