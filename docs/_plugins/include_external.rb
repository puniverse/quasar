module Jekyll
  class IncludeExternal < Liquid::Tag

    def initialize(tag_name, text, tokens)
      @text = text

      begin
        f = File.new(@text.strip, "r")
        @output = f.read()
      rescue => e
        @output = "<div class=\"error\">IncludeExternal error: #{e}</div>"
      ensure
        f.close unless f.nil?
      end

      super
    end

    def render(context)
      @output
    end
  end
end

Liquid::Template.register_tag('include_external', Jekyll::IncludeExternal)
