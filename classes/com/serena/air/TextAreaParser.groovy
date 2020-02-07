package com.serena.air

class TextAreaParser {

    public static List<String> parseToList(String textAreaText) {
        List<String> result = []
        parseTextAreaLines(textAreaText){ String line ->
            result << line
        }
        return result
    }

    public static MultiMap<String, String> parseToMultiMap(String textAreaText, String delimiter) {
        return new TextAreaMultiMapBuilder(textAreaText, delimiter).parse()
    }

    public static MultiMap<String, String> parseToMultiMap(String textAreaText, String delimiter, Closure handler) {
        return new TextAreaMultiMapBuilder(textAreaText, delimiter).handler(handler).parse()
    }

    public static newMultiMapBuilder() {
        return new TextAreaMultiMapBuilder()
    }

    public static TextAreaMultiMapBuilder newMultiMapBuilder(String text) {
        return new TextAreaMultiMapBuilder(text)
    }

    public static TextAreaMultiMapBuilder newMultiMapBuilder(String text, String delimiter) {
        return new TextAreaMultiMapBuilder(text, delimiter)
    }


    private static void parseTextAreaLines(String textAreaText, Closure processor) {
        assert textAreaText != null : 'Provide some text to parse!'
        textAreaText.eachLine { fullLine ->
            processor(fullLine.trim())
        }
    }


    static class TextAreaMultiMapBuilder {
        String text
        String delimiter = '='
        List<Closure> handlers = []
        Closure onRepeatHandler
        boolean skipEmpty = true

        TextAreaMultiMapBuilder() {
        }

        TextAreaMultiMapBuilder(String text) {
            this.text = text
        }

        TextAreaMultiMapBuilder(String text, String delimiter) {
            this.text = text
            this.delimiter = delimiter
        }

        public TextAreaMultiMapBuilder text(String text) {
            this.text = text
            return this
        }

        public TextAreaMultiMapBuilder delimiter(String delimiter) {
            this.delimiter = delimiter
            return this
        }

        public TextAreaMultiMapBuilder onRepeat(Closure handler) {
            onRepeatHandler = handler
            return this
        }

        public TextAreaMultiMapBuilder skipEmpty(boolean skipEmpty) {
            this.skipEmpty = skipEmpty
            return this
        }

        public TextAreaMultiMapBuilder handler(Closure handler) {
            handlers << handler
            return this
        }

        public MultiMap<String, String> parse() {
            assert delimiter != null
            MultiMap result = new MultiMap(new LinkedHashMap())
            parseTextAreaLines(text) {String fullLine ->
                if (skipEmpty && fullLine.isEmpty()) {
                    return
                }

                String[] res = fullLine.split(delimiter, 2)
                String name
                String value
                if (res.length > 0) {
                    name = res[0].trim()
                }
                if (res.length > 1) {
                    value = res[1].trim()
                }

                boolean isRepeat = result.containsKey(name)
                if (isRepeat && onRepeatHandler != null) {
                    callHandler(onRepeatHandler, name, value, fullLine, result)
                } else {
                    handlers.each { handler ->
                        callHandler(handler, name, value, fullLine, result)
                    }
                }
                result[name] << value
            }
            return result
        }

        private static void callHandler(Closure handler, String name, String value, String fullLine, MultiMap<String, String> resultMap) {
            Class[] handlerParams = handler.getParameterTypes()
            switch (handlerParams.size()) {
                case 2:
                    handler(name, value)
                    break;
                case 3:
                    handler(name, value, fullLine)
                    break;
                case 4:
                    handler(name, value, fullLine, resultMap)
                    break;
                default:
                    throw new IllegalArgumentException('Handler closure should have 2, 3 or 4 parameters!')
            }
        }
    }
}