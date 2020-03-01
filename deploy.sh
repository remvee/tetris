set -ex

lein cljsbuild once

rm -rf deploy
git clone git@github.com:remvee/tetris.git deploy
cd deploy
git checkout -b gh-pages --track origin/gh-pages

rm -rf -- *
mkdir css js
cp ../resources/public/index.html .
cp ../resources/public/css/style.css css/
cp ../resources/public/js/tetris.js js/

git add -- *
git commit -m '..'
git push
