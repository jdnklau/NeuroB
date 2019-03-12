# Todo's

## Refactoring

### Training sets

- [x] Generation of training set
  - [x] Training data formats
    - [x] Loading of samples from the format
  - [x] Create DB
  - [x] Create features and labels
  - [x] Skip already existing data
    - [x] naively by timestamp
    - [ ] _optional_ by versioned generation step
  - [x] Generation statistics
    - [x] Return statistics after creation of data
  - [x] Wrap StateSpaces
  - [x] CLI Option to create data
  - [x] Incorporate BPredicate/BElement classes more
- [ ] Training set manipulation
  - [x] Split training set
  - [ ] upsample training set
  - [ ] downsample training set
  - [x] shuffle training set
  - [ ] shuffling of big data sets that do not fit totally into memory
- [x] Training set analysis
  - [x] Classification analysis tool
  - [x] Regression analysis tool
  - [x] PredicateDb analysis
- [x] Data Base translation
  - [x] Migrate from old pdump to new JSON
  - [x] Translate Db to Training Format

### Machine Learning algorithms

- [ ] Training of neural networks

### Other

- [x] Enhance versioning of Backends

## Planned features

- [ ] Appending to a format
- [ ] enhanced documentation
  - [x] documentation of JSON entries, (legacy) Predicate dumps, other formats
  - [ ] usage examples
    - [ ] Training data generation
    - [x] Training data migration
    - [ ] Training/using neural networks
- [ ] RNN support
  - [ ] set training set structure
    - [ ] set/implement appropriate RecordReader
  - [ ] create RNNTrainingDataGenerator(s)
  - [ ] create RNN features
    - [ ] raw predicate features
    - [ ] predicate AST features
- [ ] Data augmentation utilities
  - [ ] PCA: whitening
  - [ ] add type information to identifiers
    - [ ] hungarian notation: "x + y" -> "xInt + yInt"
    - [ ] joshua notation: "x + y" -> "i1 + i2"
- [ ] Enhanced analysis of feature sets
  - [ ] feature dimensionality reduction
    - [ ] PCA
    - [ ] RBM
    - [ ] by decision trees
  - [ ] t-SNE
- [ ] Decision trees
  - [ ] Random forests 
  - [ ] Deep Forest

