.PHONY: all
all: plugin

.PHONY: plugin
plugin:
	./gradlew assemble

.PHONY: clean
clean:
	rm -f build/libs/*.jar
	rm -f run/plugins/TurnAround.jar

.PHONY: run
run: plugin
	rm -f ./run/plugins/TurnAround.jar
	ln -sf $$(pwd)/build/libs/*.jar ./run/plugins/TurnAround.jar
	cd ./run && java -jar server.jar -nogui
