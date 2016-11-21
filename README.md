[![Spring Data LDAP](https://spring.io/badges/spring-data-ldap/ga.svg)](http://projects.spring.io/spring-data-ldap#quick-start)
[![Spring Data LDAP](https://spring.io/badges/spring-data-ldap/snapshot.svg)](http://projects.spring.io/spring-data-ldap#quick-start)

# Spring Data LDAP

The primary goal of the [Spring Data](http://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data LDAP project aims to provide familiar and consistent repository abstractions for [Spring LDAP](https://github.com/spring-projects/spring-ldap). 

## Getting Help

For a comprehensive treatment of all the Spring Data LDAP features, please refer to:

* the [User Guide](http://docs.spring.io/spring-data/ldap/docs/current/reference/html/)
* the [JavaDocs](http://docs.spring.io/spring-data/ldap/docs/current/api/) have extensive comments in them as well.
* the home page of [Spring Data LDAP](http://projects.spring.io/spring-data-ldap) contains links to articles and other resources.
* for more detailed questions, use [Spring Data LDAP on Stackoverflow](http://stackoverflow.com/questions/tagged/spring-data-ldap).

If you are new to Spring as well as to Spring Data, look for information about [Spring projects](http://projects.spring.io/).


## Quick Start

### Maven configuration

Add the Maven dependency:

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-ldap</artifactId>
  <version>${version}.RELEASE</version>
</dependency>
```

If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version.

```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-ldap</artifactId>
  <version>${version}.BUILD-SNAPSHOT</version>
</dependency>

<repository>
  <id>spring-libs-snapshot</id>
  <name>Spring Snapshot Repository</name>
  <url>http://repo.spring.io/libs-snapshot</url>
</repository>
```

### Spring Data repositories

To simplify the creation of data repositories Spring Data LDAP provides a generic repository programming model. It will automatically create a repository proxy for you that adds implementations of finder methods you specify on an interface.  

For example, given a `Person` class with first and last name properties, a `PersonRepository` interface that can query for `Person` by last name and when the first name matches a like expression is shown below:

```java
public interface PersonRepository extends CrudRepository<Person, Name> {

  List<Person> findByLastname(String lastname);

  List<Person> findByFirstnameLike(String firstname);
}
```

The queries issued on execution will be derived from the method name. Extending `CrudRepository` causes CRUD methods being pulled into the interface so that you can easily save and find single entities and collections of them.

You can have Spring automatically create a proxy for the interface by using the following JavaConfig:

```java
@Configuration
@EnableLdapRepositories
class ApplicationConfig {

}
```

This will find the repository interface and register a proxy object in the container. You can use it as shown below:

```java
@Service
public class MyService {

  private final PersonRepository repository;

  @Autowired
  public MyService(PersonRepository repository) {
    this.repository = repository;
  }

  public void doWork() {

     repository.deleteAll();

     Person person = new Person();
     person.setFirstname("Rob");
     person.setLastname("Winch");
     person = repository.save(person);

     List<Person> lastNameResults = repository.findByLastname("Winch");
     List<Person> firstNameResults = repository.findByFirstnameLike("Ro*");
 }
}
```

## Contributing to Spring Data

Here are some ways for you to get involved in the community:

* Get involved with the Spring community on Stackoverflow and help out on the [spring-data-ldap](http://stackoverflow.com/questions/tagged/spring-data-ldap) tag by responding to questions and joining the debate.
* Create [JIRA](https://jira.springframework.org/browse/DATALDAP) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
* Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
* Watch for upcoming articles on Spring by [subscribing](http://spring.io/blog) to spring.io.

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup).  Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do.  Active contributors might be asked to join the core team, and given the ability to merge pull requests.
