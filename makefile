build-image:
	docker build -t kotlin-android-builder .

build-apk:
	@if [ -z "$$(docker images -q kotlin-android-builder)" ]; then \
		$(MAKE) build-image; \
	fi
	docker run --rm -v `pwd`:/workspace -w /workspace kotlin-android-builder
