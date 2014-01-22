#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG Commit: $TRAVIS_COMMIT\n"

email="info@paralleluniverse.co"
username="PU Bot"
site_dir=docs/_site

function error_exit
{
	echo -e "\e[01;31m$1\e[00m" 1>&2
	exit 1
}

if [ "$TRAVIS_BRANCH" == "$DOCS_BRANCH" ]; then
	echo -e "Installing Jekyll...\n"
	gem install kramdown
	gem install jekyll
	gem install typogruby
	gem install nokogiri

	if [ ! -z "$GEN_APIDOCS" ]; then
		echo -e "Generating API docs...\n"
		echo -e "Running: $GEN_APIDOCS\n"
		eval $GEN_APIDOCS
	fi

	# build site
	echo -e "Building Jekyll site...\n"
	cd docs
	jekyll build || error_exit "Error building Jekyll site"
	cd ..

	echo -e "Updating gh-pages...\n"
    # Any command that using GH_OAUTH_TOKEN must pipe the output to /dev/null to not expose your oauth token
    git submodule add -b gh-pages https://${GH_OAUTH_TOKEN}@github.com/$TRAVIS_REPO_SLUG site > /dev/null 2>&1
    cd site
	git checkout gh-pages || git checkout -b gh-pages
    git rm -r .
    cp -R ../$site_dir/* .
    cp ../$site_dir/.* .
    touch .nojekyll
    git add -f .
    git config user.email $email
    git config user.name $username
    git commit -am "Travis build $TRAVIS_BUILD_NUMBER, commit $TRAVIS_COMMIT, pushed to gh-pages"
    # Any command that using GH_OAUTH_TOKEN must pipe the output to /dev/null to not expose your oauth token
    git push https://${GH_OAUTH_TOKEN}@github.com/$TRAVIS_REPO_SLUG HEAD:gh-pages > /dev/null 2>&1 || error_exit "Error updating gh-pages"

	#git checkout -B gh-pages
	#git add -f dist/.
	#git commit -q -m "Travis build $TRAVIS_BUILD_NUMBER pushed to gh-pages"
	#git push -fq upstream gh-pages 2> /dev/null || error_exit "Error updating gh-pages"

	echo -e "Finished updating gh-pages\n"
fi

end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Post-build process finished in $minutes minute(s) and $seconds seconds"