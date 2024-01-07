.PHONY: all
all: plugin

.PHONY: plugin
plugin:
	./gradlew assemble

.PHONY: clean
clean:
	bash plugin/clean.sh
	bash server/clean.sh
	bash resourcepack/clean.sh

.PHONY: run
run: plugin
	rm -f ./run/plugins/TurnAround.jar
	ln -sf $$(pwd)/build/libs/*.jar ./run/plugins/TurnAround.jar
	cd ./run && java -jar server.jar -nogui
