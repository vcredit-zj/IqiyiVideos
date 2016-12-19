# Video Downloader
This simple tool can help you download videos from multiple URLs and put them into separate folders named after corresponding video titles or into a single folder based on your instruction.

By default, it uses You-Get as the downloading tool.

## Features:
* No need to change source code after you update YouGet
* Manage URLs in target list
* Fetch and show video titles of targets (with cache)
* Save current target list into a .json file
* Load target list from a .json file
* Download all targets into a single folder
* Download all targets into separate folders named after their titles
* Allow user to specify the quality level of targets to be downloaded (not available for all sites)
* Multiple targets can be downloaded at the same time
* Report any failed targets to the user

## Dependencies
[Gson](https://github.com/google/gson) is used to serialize and deserialize Java Objects into JSON and back.

## Usage
Change the values of constants in the Controller class before you run it. You can get YouGet from [here](https://github.com/soimort/you-get).

## License
This software is distributed under the [MIT license](https://github.com/ad52825196/video-downloader/raw/master/LICENSE).
