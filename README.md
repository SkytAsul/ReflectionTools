# Reflection Mappings Tools
Two tools to make reflection easy when used on remapped software.

## reflection-remapper
![Maven Central Version](https://img.shields.io/maven-central/v/fr.skytasul/reflection-remapper)
```xml
<dependency>
    <groupId>fr.skytasul</groupId>
    <artifactId>reflection-remapper</artifactId>
    <version>{VERSION}</version>
    <scope>compile</scope>
</dependency>
```
This util is the "userland" one: it is used at runtime to make your reflection calls on remapped software.

## reflection-mappings-shrieker
![Maven Central Version](https://img.shields.io/maven-central/v/fr.skytasul/reflection-mappings-shrieker)
```xml
<dependency>
    <groupId>fr.skytasul</groupId>
    <artifactId>reflection-mappings-shrieker</artifactId>
    <version>{VERSION}</version>
    <scope>provided</scope>
</dependency>
```
This util is only used during development to generate mapping files. Its main interest is to shrink huge mapping files to much smaller ones, only containing the necessary mappings. It can also merge multiple mapping files into one.

## Example
See my util [GlowingEntities](https://github.com/SkytAsul/GlowingEntities) which uses those tools.