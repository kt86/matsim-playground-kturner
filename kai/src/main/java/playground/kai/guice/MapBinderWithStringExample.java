package playground.kai.guice;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MapBinderWithStringExample{
	private static final Logger log = Logger.getLogger( MapBinderWithStringExample.class ) ;

	public static void main ( String [] args ) {
		new MapBinderWithStringExample().run() ;
	}

//	@Inject Map<Annotation, Set<Provider<MyInterface>>> map ;

	void run() {
		List<Module> modules = new ArrayList<>() ;
		modules.add(  new AbstractModule(){
			@Override
			protected void configure(){
				MapBinder<String, MyInterface> mapBinder = MapBinder.newMapBinder( this.binder(), String.class, MyInterface.class );
//				mapBinder.permitDuplicates() ;
				mapBinder.addBinding("abc" ).to( MyImpl1.class ) ;
//				mapBinder.addBinding("abc" ).to( MyImpl2.class ) ;

			}
		} ) ;
		Injector injector = Guice.createInjector( modules );

		Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

		for( Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet() ){
			log.info("") ;
			log.info( "key=" + entry.getKey() ) ;
			log.info( "value=" + entry.getValue() ) ;
		}
		log.info("") ;

		Map<String, Provider<MyInterface>> map = injector.getInstance( Key.get( new TypeLiteral<Map<String, Provider<MyInterface>>>(){} ) );;
		Provider<MyInterface> provider = map.get( "abc" );;

//		for( Provider<MyInterface> provider : set ){
			provider.get() ;
//		}

	}

	private interface MyInterface{

	}

	private static class MyImpl1 implements MyInterface{
		@Inject MyImpl1() {
			log.info( "ctor 1 called" );
		}
	}

	private static class MyImpl2 implements MyInterface{
		@Inject MyImpl2() {
			log.info( "ctor 2 called" );
		}
	}
}
