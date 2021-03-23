#  Dependency injection

This is Batch's Dependency Injection annotation processor.

It generate classes/methods to fetch instances of objects. It doesn't manage a dependency graph yet.

## Create a module

To create a module, you need to annotate the class with the @Module annotation. Doing this will generate a Provider class, allowing you to create or get instances of this class.

Note: For a class to be a Module it needs to be public, and have at least one public constructor.

# Singleton

If you want your module to respect the singleton pattern, just add the @Singleton annotation on the class (at the same place of @Module).

# Provide a custom instantiation

If you want your constructor to remain private, or if you want to add dependency from other module, you need to add a provide method.

Ex:

```java
@Module
@Singleton
public class TestModule {
    private TestModule(OtherModule module) {
        ...
    }

    @Provide
    public static TestModule provide() {
        return new TestModule(OtherModuleProvider.get());
    }
}
```

In this case, the TestModuleProvider will call the provide() method instead of the constructor.

## Providing a new implementation

Use the Provider#get() method to get a instance of the class.

```java
TestModule instance = TestModuleProvider.get();
```

## Testing

A huge part of dependency injection is to be able to change what is injected in tests.

The DITestUtils class allows to easily to do this by letting you to mock a singleton and release all previously instantiated module.
