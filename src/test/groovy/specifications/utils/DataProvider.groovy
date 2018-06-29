package specifications.utils

class DataProvider implements Iterable{

    def object_list = []
    private int counter

  /* void setObjectList(List objectList){
        object_list << objectList
    }*/

    @Override
    Iterator iterator() {
        [
                hasNext: {
                    counter < object_list.size()
                },
                next: {
                    object_list[counter++]
                }
        ] as Iterator
    }

}
