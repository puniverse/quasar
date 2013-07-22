# Strips https?:// from a URL.
# Usage: {{ page.url | domain_name }}
# https://github.com/LawrenceWoodman/domain_name-liquid_filter

require 'liquid'

module DomainNameFilter

  # Return the url's domain name
  def domain_name(url)
    return url.sub(%r{(https?://){0,1}([^/]*)(/.*$){0,1}}i, '\\2\\3')
  end

end

Liquid::Template.register_filter(DomainNameFilter)
