#!/bin/bash
echo
echo "Deploying docs ..."
echo

# This creates Javadocs docs in $TRAVIS_BUILD_DIR/build/docs/javadoc with Javadoc
./gradlew javadoc

# This creates WebAPI docs in $TRAVIS_BUILD_DIR/build/asciidoc/html5 with Asciidoc
./gradlew asciidoc

if [ -n "$GITHUB_API_KEY" ]; then
    cd "$TRAVIS_BUILD_DIR"/docs-build        
    rm -r -f sitmun.github.io
    git clone https://github.com/sitmun/sitmun.github.io.git
    cd sitmun.github.io
    cp -r "$TRAVIS_BUILD_DIR"/build/docs/javadoc  ./doc-java
    cp -r "$TRAVIS_BUILD_DIR"/build/asciidoc/html5  ./doc-rest
    git add doc-java/*
    git add doc-rest/*
    git commit -m "Automatic update of the docs"
    # Make sure to make the output quiet, or else the API token will leak!
    # This works because the API key can replace your password.
    git push -q https://rbejar:$GITHUB_API_KEY@github.com/sitmun/sitmun.github.io master &>/dev/null    
fi
