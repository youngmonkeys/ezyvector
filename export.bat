mvn -pl . clean install & ^
mvn -pl ezyvector-sdk clean install & ^
mvn -pl ezyvector-admin-plugin clean install -Pexport,\!test & ^
mvn -pl ezyvector-web-plugin clean install -Pexport,\!test & ^
ezy.bat package & ^
ezy.bat export
