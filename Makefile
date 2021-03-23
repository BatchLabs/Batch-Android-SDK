.PHONY: clean aar

aar:
	cd Sources/ && ./gradlew clean
	cd Sources/ && ./gradlew :sdk:assembleRelease --no-build-cache
	cp Sources/sdk/build/outputs/aar/Batch-release.aar public-sdk/Batch.aar
	cp Sources/sdk/build/outputs/mapping/release/mapping.txt public-sdk/Batch.mapping.txt
	sh ./copy_release_mapping.sh

clean:
	rm -f public-sdk/Batch.aar
	cd Sources && ./gradlew clean