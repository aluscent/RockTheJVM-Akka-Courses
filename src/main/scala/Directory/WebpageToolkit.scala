package Directory

object WebpageToolkit {

  def buildHtml(head: String = "", body: String = ""): String =
    s"""<!DOCTYPE html>
       |<html lang="en">
       |  <head>
       |    <meta charset="UTF-8">
       |    <meta name="viewport" content="width=device-width, initial-scale=1">
       |    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet"
       |      integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC" crossorigin="anonymous">
       |    $head
       |  </head>
       |  <body>
       |  <div class="container">
       |  <div class="row row-cols-2">
       |    $body
       |    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js"
       |      integrity="sha384-MrcW6ZMFYlzcLA8Nl+NtUVF0sA7MsXsP1UyJoMp4YLEuNSfAP+JcXn/tWtIaxVXM" crossorigin="anonymous"></script>
       |  </div>
       |  </div>
       |  </body>
       </html>""".stripMargin

  def buildForm(components: List[String]): String =
    s"""<div class="col-lg-auto">
       |<form action="http://localhost:12345" method="get">
       |  ${components.mkString("\n")}
       |  <input type="submit" value="Submit" class="btn btn-primary">
       |</form>
       |</div>
       |""".stripMargin

  def buildInput(kind: String, name: String): String =
    s"""<div class="mb-3">
       |<label for="$name" class="form-label">${name.capitalize}</label>
       |<input type="$kind" name="$name" id="$name" class="form-control" />
       |</div>
       |""".stripMargin

  def buildRadio(name: String, values: List[String]): String =
    s"""<label class="form-check-label">Select ${name.capitalize}:</label>
       |<div class="form-check">
       |  ${values.map(value => s"""<input type="radio" name="$name" value="$value" id="$value" class="form-check-input" /><label for="$value" class="form-check-label">$value</label>""").mkString("<br>\n")}
       |</div>
       |""".stripMargin
}
