# Build outerjar.jar which has innerjar.jar inside which has fake Foo.class and Bar.class in it
echo "hello" > Foo.class
echo "world" > Bar.class
jar -cvMf innerjar.jar Foo.class Bar.class
mkdir lib
mv innerjar.jar lib
jar -cvMf outerjar.jar lib
rm Foo.class Bar.class
rm lib/innerjar.jar
rmdir lib

# Build simplejar.jar which has com/foo/Xxx.class and com/bar/Yyy.class in it
mkdir -p com/foo
echo "fake" > com/foo/Xxx.class
mkdir -p com/bar
echo "fake" > com/bar/Yyy.class
jar -cvMf simplejar.jar com
rm com/foo/Xxx.class
rm com/bar/Yyy.class
rmdir com/foo
rmdir com/bar
rmdir com

