# Jar 'outerjar.jar' is simply created by:
echo "hello" > Foo.class
echo "world" > Bar.class
jar -cvMf innerjar.jar Foo.class Bar.class
mkdir lib
mv innerjar.jar lib
jar -cvMf outerjar.jar lib
rm Foo.class Bar.class
rm lib/innerjar.jar
rmdir lib
