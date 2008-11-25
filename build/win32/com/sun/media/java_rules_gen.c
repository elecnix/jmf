
#include <stdio.h>
#include <string.h>

int
main(int argc, char **argv)
{
    int arg;
    for (arg = 1; arg < argc; arg++) {
	char *classes = argv[arg];
	char *temp;
	char *last;
	while (!(classes[0] == 'c' &&
		 classes[1] == 'o' &&
		 classes[2] == 'm'))
	    classes++;
	temp = classes;
	last = classes + strlen(classes);
	while (*last != '/' &&
	       *last != '\\')
	    last--;
	*last = 0;
	while (*temp++ != 0)
	    if (*temp == '/')
		*temp = '\\';
	printf("{$(SRCREFDIR)\\%s}.java{$(DESTDIR)\\%s}.class:\n"
	       "\t$(ECHO) ---- Compiling $<\n"
	       "\t$(JAVAC) $<\n\n", classes, classes);
	printf("{$(SRCDIR)\\%s}.java{$(DESTDIR)\\%s}.class:\n"
	       "\t$(ECHO) ---- Compiling $<\n"
	       "\t$(JAVAC) $<\n\n", classes, classes);

    }
    return 0;
}
