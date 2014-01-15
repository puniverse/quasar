require 'nokogiri'

module ExcludeTocFilter
  def exclude_toc(html)
    doc = Nokogiri::HTML(html)
    doc.css('#markdown-toc').remove
    doc.to_html
  end
end
Liquid::Template.register_filter(ExcludeTocFilter)